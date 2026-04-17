package com.atguigu.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.constant.product.CategoryConstant;
import com.atguigu.common.vo.product.Catalog2VO;
import com.atguigu.common.vo.product.Catalog2VO;
import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.common.entity.product.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;
    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    RedissonClient redisson;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {

        List<CategoryEntity> entities = baseMapper.selectList(null);

        List<CategoryEntity> level1Menus = entities.stream().filter(categoryEntity ->
                categoryEntity.getParentCid() == 0
        ).map((menu) -> {
            menu.setChildren(getChildren(menu, entities));
            return menu;
        }).sorted((menu1, menu2) -> {
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());

        return level1Menus;
    }

    @Override
    public void removeMenuByIds(List<Long> list) {
        //TODO 1.检查当前删除的菜单，是否被别的地方引用
        baseMapper.deleteBatchIds(list);
    }

    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        List<Long> parentPath = findParentPath(catelogId, paths);
        Collections.reverse(parentPath);

        return paths.toArray(new Long[parentPath.size()]);
    }

    /**
     * 级联更新所有关联表的冗余数据
     * 缓存策略：失效模式，方法执行完删除缓存
     */
    @CacheEvict(value = {"category"}, allEntries = true)
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());

    }

    /**
     * 查出所有1级分类
     */
    @Cacheable(value = {"category"}, key = "'getLevel1Categorys'", sync = true)
    @Override
    public List<CategoryEntity> getLevel1Categorys() {
        System.out.println("调用了getLevel1Categorys...");
        // 查询父id=0
        return baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
    }

    @Override
    public Map<String, List<Catalog2VO>> getCatalogJson() {
        String cataLogJSON = redisTemplate.opsForValue().get("catalogJSON");
        if (StringUtils.isEmpty(cataLogJSON)) {
            // 查询成功直接返回不需要查询DB
            Map<String, List<Catalog2VO>> catalogJsonFromDb = getCatalogJsonFromDBWithRedisLock();
            String s = JSON.toJSONString(catalogJsonFromDb);
            redisTemplate.opsForValue().set("catalogJSON", s);
            return catalogJsonFromDb;
        }
        Map<String, List<Catalog2VO>> result = JSON.parseObject(cataLogJSON, new TypeReference<Map<String, List<Catalog2VO>>>() {
        });
        return result;
    }

    /**
     * 查询三级分类（redisson分布式锁版本）
     */
    public Map<String, List<Catalog2VO>> getCatalogJsonFromDBWithRedissonLock() {
        // 1.抢占分布式锁，同时设置过期时间
        RLock lock = redisson.getLock(CategoryConstant.LOCK_KEY_CATALOG_JSON);
        lock.lock(30, TimeUnit.SECONDS);
        try {
            // 2.查询DB
            Map<String, List<Catalog2VO>> result = getDataFromDb();
            return result;
        } finally {
            // 3.释放锁
            lock.unlock();
        }
    }

    /**
     * 查询三级分类（原生版redis分布式锁版本）
     */
    public Map<String, List<Catalog2VO>> getCatalogJsonFromDBWithRedisLock() {
        // 1.抢占分布式锁，同时设置过期时间
        String uuid = UUID.randomUUID().toString();
        // 使用setnx占锁（setIfAbsent）
        Boolean isLock = redisTemplate.opsForValue().setIfAbsent(CategoryConstant.LOCK_KEY_CATALOG_JSON, uuid, 300, TimeUnit.SECONDS);
        if (isLock) {
            // 2.抢占成功
            Map<String, List<Catalog2VO>> result = null;
            try {
                // 查询DB
                // TODO 业务续期（锁过期）【不应该添加业务续期代码】
                return getDataFromDb();
            } finally {
                // 3.查询UUID是否是自己，是自己的lock就删除
                // 封装lua脚本（原子操作解锁）
                // 查询+删除（当前值与目标值是否相等，相等执行删除，不等返回0）
                String luaScript = "if redis.call('get',KEYS[1]) == ARGV[1]\n" +
                        "then\n" +
                        "    return redis.call('del',KEYS[1])\n" +
                        "else\n" +
                        "    return 0\n" +
                        "end";
                // 删除锁
                redisTemplate.execute(new DefaultRedisScript<Long>(luaScript, Long.class), Arrays.asList(CategoryConstant.LOCK_KEY_CATALOG_JSON), uuid);
            }
        } else {
            // 4.加锁失败，自旋重试
            // TODO 不应该使用递归，使用while，且固定次数
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return getCatalogJsonFromDBWithRedisLock();
        }
    }

    /**
     * 查询三级分类（从数据源DB查询）
     * 加入分布式锁版本代码，double check检查
     */
    public Map<String, List<Catalog2VO>> getCatalogJsonFromDBWithLocalLock() {
        synchronized (this) {
            return getDataFromDb();
        }
    }

    private Map<String, List<Catalog2VO>> getDataFromDb() {
        String catlogJSON = redisTemplate.opsForValue().get("catlogJSON");
        if (!StringUtils.isEmpty(catlogJSON)) {
            // 查询成功直接返回不需要查询DB
            Map<String, List<Catalog2VO>> result = JSON.parseObject(catlogJSON, new TypeReference<Map<String, List<Catalog2VO>>>() {
            });
            return result;
        }

        // 2.查询所有分类，按照parentCid分组
        Map<Long, List<CategoryEntity>> categoryMap = baseMapper.selectList(null).stream()
                .collect(Collectors.groupingBy(key -> key.getParentCid()));

        // 3.获取1级分类
        List<CategoryEntity> level1Categorys = categoryMap.get(0L);

        // 4.封装数据
        Map<String, List<Catalog2VO>> result = level1Categorys.stream().collect(Collectors.toMap(key -> key.getCatId().toString(), l1Category -> {
            // 3.查询2级分类，并封装成List<Catalog2VO>
            List<Catalog2VO> Catalog2VOS = categoryMap.get(l1Category.getCatId())
                    .stream().map(l2Category -> {
                        // 4.查询3级分类，并封装成List<Catalog3VO>
                        List<Catalog2VO.Catalog3Vo> catalog3Vos = categoryMap.get(l2Category.getCatId())
                                .stream().map(l3Category -> {
                                    // 封装3级分类VO
                                    Catalog2VO.Catalog3Vo catalog3Vo = new Catalog2VO.Catalog3Vo(l2Category.getCatId().toString(), l3Category.getCatId().toString(), l3Category.getName());
                                    return catalog3Vo;
                                }).collect(Collectors.toList());
                        // 封装2级分类VO返回
                        Catalog2VO Catalog2VO = new Catalog2VO(l1Category.getCatId().toString(), catalog3Vos, l2Category.getCatId().toString(), l2Category.getName());
                        return Catalog2VO;
                    }).collect(Collectors.toList());
            return Catalog2VOS;
        }));

        // 5.结果集存入redis
        // 关注锁时序问题，存入redis代码块必须在同步快内执行
        redisTemplate.opsForValue().set(CategoryConstant.CACHE_KEY_CATALOG_JSON,
                JSONObject.toJSONString(result), 1, TimeUnit.DAYS);

        return result;
    }

    private List<Long> findParentPath(Long catelogId, List<Long> paths) {
        paths.add(catelogId);
        CategoryEntity byId = this.getById(catelogId);
        if (byId.getParentCid() != 0) {
            findParentPath(byId.getParentCid(), paths);
        }
        return paths;
    }

    private List<CategoryEntity> getChildren(CategoryEntity root, List<CategoryEntity> all) {

        List<CategoryEntity> children = all.stream().filter((categoryEntity) -> {
            return categoryEntity.getParentCid() == root.getCatId();
        }).map(categoryEntity -> {
            categoryEntity.setChildren(getChildren(categoryEntity, all));
            return categoryEntity;
        }).sorted((menu1, menu2) -> {
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());

        return children;
    }
}