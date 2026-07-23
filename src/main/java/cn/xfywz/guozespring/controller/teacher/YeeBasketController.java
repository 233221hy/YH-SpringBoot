package cn.xfywz.guozespring.controller.teacher;

import cn.xfywz.guozespring.annotation.RequireAuth;
import cn.xfywz.guozespring.entity.mhsch.YeeBasket;
import cn.xfywz.guozespring.entity.mhsch.YeeQuestion;
import cn.xfywz.guozespring.service.teacher.YeeBasketService;
import cn.xfywz.guozespring.service.teacher.YeeQuestionService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @Author: ChengLin
 * 试题篮筐 yee_basket
 */
@RequireAuth
@RestController
@RequestMapping("/school")
public class YeeBasketController {

    @Autowired
    private YeeBasketService yeeBasketService;
    @GetMapping("/yee_basket_list")
    public Result selectAll(@RequestParam int schoolId,
                            @RequestParam Integer userId) throws Exception{
        return yeeBasketService.selectAll(schoolId, userId);
    }
    @PostMapping("/yee_basket_add")
    public Result add(@RequestBody YeeBasket yeeBasket) throws Exception{
        return yeeBasketService.add(yeeBasket);
    }

    @PostMapping("/yee_basket_delete")
    public Result delete(@RequestParam int schoolId,
                         @RequestParam int id,
                         @RequestParam Integer userId) throws Exception{
        return yeeBasketService.delete(schoolId, id, userId);
    }

    @PostMapping("/yee_basket_deleteAll")
    public Result deleteAll(@RequestParam int schoolId,
                         @RequestParam int userId) throws Exception{
        return yeeBasketService.deleteAll(schoolId, userId);
    }

}
