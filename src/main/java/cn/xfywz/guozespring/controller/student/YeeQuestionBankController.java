package cn.xfywz.guozespring.controller.student;


import cn.xfywz.guozespring.service.student.YeeQuestionBankService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class YeeQuestionBankController {

    @Autowired
    private YeeQuestionBankService yeeCollectionService;

    //获取收藏题库列表
    @GetMapping("/yee_collection_topic_list")
    public Result list(@RequestParam int schoolId,
                       @RequestParam int studentId,
                       @RequestParam int pageSize,
                       @RequestParam int pageNum,
                       @RequestHeader String Authorization) throws Exception {
        if (Authorization != null) {
            return yeeCollectionService.selectAll(schoolId,studentId,pageSize, pageNum);
        } else return Result.error("非法访问");
    }

    //收藏题目详情
    @GetMapping("/yee_collection_detail")
    public Result detail(@RequestParam int schoolId,
                         @RequestParam int chapterId,
                         @RequestParam long studentId,
                         @RequestHeader String Authorization) throws Exception {
        if (Authorization != null) {
            return yeeCollectionService.selectById(schoolId,chapterId, studentId);
        } else return Result.error("非法访问");
    }

    //获取收藏题库列表
//    @GetMapping("/yee_wrong_topic_list")


}
