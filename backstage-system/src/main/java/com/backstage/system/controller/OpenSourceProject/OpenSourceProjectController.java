package com.backstage.system.controller.OpenSourceProject;

import com.backstage.common.annotation.Anonymous;
import com.backstage.common.annotation.Log;
import com.backstage.common.core.controller.BaseController;
import com.backstage.common.core.domain.R;
import com.backstage.common.core.page.TableDataInfo;
import com.backstage.common.enums.BusinessType;
import com.backstage.system.domain.openSourceProject.OpenSourceProject;
import com.backstage.system.service.OpenSourceProject.OpenSourceProjectService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


import com.backstage.common.core.page.TableDataInfo;

import java.util.List;
import java.util.Map;

import static com.backstage.common.utils.PageUtils.startPage;

@RestController
@RequestMapping("/pc/opensource")
public class OpenSourceProjectController extends BaseController {

    @Autowired
    private OpenSourceProjectService openSourceProjectService;

    /**
     * 新增开源项目
     */
    @Anonymous
    @Log(title = "开源项目", businessType = BusinessType.INSERT)
    @ApiOperation("新增开源项目")
    @PostMapping
    public R<Void> add(@RequestBody OpenSourceProject openSourceProject)
    {
        int addResult = openSourceProjectService.add(openSourceProject);
        if (addResult>0) {
            return R.ok();
        } else {
            return R.fail("新增开源项目失败");
        }
    }

    /**
     * 修改开源项目
     */
    @Anonymous
    @Log(title = "开源项目", businessType = BusinessType.UPDATE)
    @ApiOperation("修改开源项目")
    @PutMapping
    public R<Void> edit(@RequestBody OpenSourceProject openSourceProject) {
        int updateResult = openSourceProjectService.updateOpenSourceProject(openSourceProject);
        if (updateResult > 0) {
            return R.ok();
        } else {
            return R.fail("修改开源项目失败");
        }
    }

    /**
     * 下架开源项目 逻辑删除
     */
    @Anonymous
    @Log(title = "开源项目", businessType = BusinessType.DELETE)
    @ApiOperation("下架开源项目")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        int deleteResult = openSourceProjectService.deleteOpenSourceProjectById(id);
        if (deleteResult > 0) {
            return R.ok();
        } else {
            return R.fail("下架开源项目失败");
        }
    }


    /**
     * 根据id查询开源项目详情
     */
    @Anonymous
    @ApiOperation("根据id查找开源项目")
    @GetMapping("/{id}")
    public R<OpenSourceProject> getById(@PathVariable Long id) {
        OpenSourceProject openSourceProject = openSourceProjectService.selectOpenSourceProjectById(id);
        return R.ok(openSourceProject);

    }

    /**
     * 查询开源项目列表
     */
//@PreAuthorize("@ss.hasPermi('system:opensource:list')")
    @Anonymous
    @ApiOperation("查找开源项目列表")
    @GetMapping("/list")
    public TableDataInfo list(OpenSourceProject openSourceProject){
        startPage();
        List<OpenSourceProject> getOpenSourceProjectList = openSourceProjectService.selectOpenSourceProjectList(openSourceProject);
        return getDataTable(getOpenSourceProjectList);

    }

}
