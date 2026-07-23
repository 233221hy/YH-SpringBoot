package cn.xfywz.guozespring.controller.admin;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.xfywz.guozespring.entity.mhmain.SlManageNode;
import cn.xfywz.guozespring.service.admin.SlManageNodeService;
import cn.xfywz.guozespring.util.Result;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static cn.xfywz.guozespring.constant.CacheConstant.CACHE_KEY_SEPARATOR;
import static cn.xfywz.guozespring.constant.CacheConstant.USER_PERMISSION_LIST_CACHE_KEY;

@RestController
@RequestMapping("/manage/node")
public class SlManageNodeController {

    @Autowired
    private SlManageNodeService slManageNodeService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 获取角色权限列表 redis
     */
    @SaCheckLogin
    @GetMapping("/getRoleList/{id}")
    public Result getRoleList(@PathVariable("id") String id) {
        List<String> userPermissionList = (List<String>) redisTemplate.opsForValue().get(USER_PERMISSION_LIST_CACHE_KEY + CACHE_KEY_SEPARATOR + id);
        return Result.success(userPermissionList);
    }

    /**
     * 添加节点
     */
    @PostMapping("/add")
    public Result add(@RequestBody SlManageNode slManageNode) {
        try {
            boolean save = slManageNodeService.save(slManageNode);
            if (save) {
                return Result.success("添加成功");
            } else {
                return Result.error("添加失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("添加失败：" + e.getMessage());
        }
    }

    /**
     * 根据ID删除节点
     */
    @GetMapping("/delete/{id}")
    public Result delete(@PathVariable long id) {
        try {
            boolean remove = slManageNodeService.removeById(id);
            if (remove) {
                return Result.success("删除成功");
            } else {
                return Result.error("删除失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("删除失败：" + e.getMessage());
        }
    }

    /**
     * 批量删除节点
     */
    @PostMapping("/batchDelete")
    public Result batchDelete(@RequestParam List<Long> ids) {
        try {
            boolean remove = slManageNodeService.removeByIds(ids);
            if (remove) {
                return Result.success("批量删除成功");
            } else {
                return Result.error("批量删除失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("批量删除失败：" + e.getMessage());
        }
    }

    /**
     * 更新节点
     */
    @PostMapping("/update")
    public Result update(@RequestBody SlManageNode slManageNode) {
        try {
            boolean update = slManageNodeService.updateById(slManageNode);
            if (update) {
                return Result.success("更新成功");
            } else {
                return Result.error("更新失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("更新失败：" + e.getMessage());
        }
    }

    /**
     * 根据ID获取节点
     */
    @GetMapping("/get/{id}")
    public Result get(@PathVariable long id) {
        try {
            SlManageNode slManageNode = slManageNodeService.getById(id);
            if (slManageNode != null) {
                return Result.success(slManageNode);
            } else {
                return Result.error("未找到该节点");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 分页查询节点列表
     */
    @GetMapping("/list")
    public Result list(@RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize,
                       @RequestParam(required = false) String name) {
        try {
            Page<SlManageNode> page = new Page<>(pageNum, pageSize);
            QueryWrapper<SlManageNode> queryWrapper = new QueryWrapper<>();
            
            if (name != null && !name.isEmpty()) {
                queryWrapper.like("name", name);
            }
            
            queryWrapper.orderByAsc("sort");
            
            Page<SlManageNode> result = slManageNodeService.page(page, queryWrapper);
            return Result.success(result.getRecords(), result.getTotal());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 获取所有节点列表
     */
    @GetMapping("/listAll")
    public Result listAll() {
        try {
            List<SlManageNode> list = slManageNodeService.list();
            return Result.success(list);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 获取所有节点列表（树形结构）
     */
    @GetMapping("/tree")
    public Result tree() {
        try {
            // 获取所有节点
            List<SlManageNode> allNodes = slManageNodeService.list();
            
            // 构建树形结构
            List<SlManageNode> tree = buildTree(allNodes);
            
            return Result.success(tree);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询失败：" + e.getMessage());
        }
    }
    
    /**
     * 构建树形结构
     * @param nodes 所有节点列表
     * @return 树形结构列表
     */
    private List<SlManageNode> buildTree(List<SlManageNode> nodes) {
        // 创建根节点列表（pid为0的节点）
        List<SlManageNode> rootNodes = new ArrayList<>();
        
        // 创建Map用于快速查找节点
        Map<Long, SlManageNode> nodeMap = new HashMap<>();
        for (SlManageNode node : nodes) {
            nodeMap.put(node.getId(), node);
        }
        
        // 构建树形结构
        for (SlManageNode node : nodes) {
            if (node.getPid() == 0) {
                // 根节点
                rootNodes.add(node);
            } else {
                // 子节点，找到父节点并添加到父节点的children中
                SlManageNode parent = nodeMap.get(node.getPid());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(node);
                }
            }
        }
        
        // 对每个节点的子节点按sort字段排序
        sortChildren(rootNodes);
        
        return rootNodes;
    }
    
    /**
     * 递归对子节点进行排序
     * @param nodes 节点列表
     */
    private void sortChildren(List<SlManageNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        
        // 对当前层级节点按sort字段排序
        nodes.sort(Comparator.comparingLong(SlManageNode::getSort));
        
        // 递归对子节点排序
        for (SlManageNode node : nodes) {
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                sortChildren(node.getChildren());
            }
        }
    }
}