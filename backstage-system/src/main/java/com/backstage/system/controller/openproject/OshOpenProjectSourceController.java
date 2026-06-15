package com.backstage.system.controller.openproject;

import com.backstage.common.annotation.Anonymous;
import com.backstage.common.core.domain.R;
import com.backstage.common.annotation.OshUserLevel;
import com.backstage.system.domain.openproject.OshOpenProjectSource;
import com.backstage.system.domain.openproject.dto.OpenProjectSourceDTO;
import com.backstage.system.service.openproject.IOshOpenProjectSourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/pc/openproject/source")
public class OshOpenProjectSourceController {

    @Autowired
    private IOshOpenProjectSourceService sourceService;

    @GetMapping("/list")
    @Anonymous
    public R<List<OshOpenProjectSource>> list() {
        return R.ok(sourceService.listSources());
    }

    @PostMapping("/save")
    @OshUserLevel(value = 4)
    public R<OshOpenProjectSource> save(@RequestBody OpenProjectSourceDTO dto) {
        try {
            return R.ok(sourceService.saveSource(dto));
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/delete")
    @OshUserLevel(value = 4)
    public R<Void> delete(@RequestParam Long id) {
        try {
            sourceService.deleteSource(id);
            return R.ok();
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/sync")
    @OshUserLevel(value = 4)
    public R<Integer> sync(@RequestParam Long id) {
        try {
            return R.ok(sourceService.syncSource(id));
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/sync-all")
    @OshUserLevel(value = 4)
    public R<Integer> syncAll() {
        return R.ok(sourceService.syncAllEnabledSources());
    }
}
