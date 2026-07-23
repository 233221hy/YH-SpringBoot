package cn.xfywz.guozespring.controller.admin;

import cn.xfywz.guozespring.entity.mhmain.SlDocument;
import cn.xfywz.guozespring.service.admin.SlDocumentService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/manage")
public class SlDocumentController {
    @Autowired
    private SlDocumentService slDocumentService;
//    @GetMapping("/document_list")
//    public Result selectAll(@RequestParam int PageSize, @RequestParam int PageNum){
//        return slDocumentService.list(PageNum,PageSize);
//    }
    @GetMapping("/document_like")
    public Result selectLike(@RequestParam String name){
        return slDocumentService.selectLike(name);
    }
    @GetMapping("/document_del")
        public Result delete(@RequestParam Integer id){
        return slDocumentService.delete(id);
        }
    @PostMapping("/document_add")
    public Result add(SlDocument slDocument){
        return slDocumentService.add(slDocument);
    }
    @PostMapping("/document_update")
    public Result update(SlDocument slDocument){
        return slDocumentService.update(slDocument);
    }
}
