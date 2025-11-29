package com.sena.getback.controller.api;

import com.sena.getback.model.ActivityLog;
import com.sena.getback.service.ActivityLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class ApiAdmin {

    @Autowired
    private ActivityLogService activityLogService;

    @GetMapping("/activity")
    public ResponseEntity<List<ActivityLog>> getRecent(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false, defaultValue = "10") Integer limit
    ) {
        List<ActivityLog> logs;
        if (page != null || size != null) {
            int p = page != null ? page : 0;
            int s = size != null ? size : 10;
            logs = activityLogService.getPage(p, s);
        } else {
            logs = activityLogService.getRecent(limit != null ? limit : 10);
        }
        for (ActivityLog a : logs) {
            a.setUsername(activityLogService.resolveDisplayUsername(a.getUsername()));
        }
        return ResponseEntity.ok(logs);
    }

    @PostMapping("/activity")
    public ResponseEntity<Map<String, Object>> create(
            @RequestParam String type,
            @RequestParam String message,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String metadata
    ) {
        ActivityLog saved = activityLogService.log(type, message, username, metadata);
        Map<String, Object> resp = new HashMap<>();
        resp.put("id", saved.getId());
        resp.put("status", "ok");
        return ResponseEntity.ok(resp);
    }
}
