package com.sena.getback.service;

import com.sena.getback.model.ActivityLog;
import com.sena.getback.repository.ActivityLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ActivityLogService {

    @Autowired
    private ActivityLogRepository repository;

    public ActivityLog log(String type, String message, String username, String metadata) {
        ActivityLog a = new ActivityLog();
        a.setTimestamp(LocalDateTime.now());
        a.setType(type);
        a.setMessage(message);
        a.setUsername(username);
        a.setMetadata(metadata);
        return repository.save(a);
    }

    public List<ActivityLog> getRecent(int limit) {
        int size = Math.max(1, Math.min(limit, 50));
        return repository.findRecent(PageRequest.of(0, size));
    }

    public List<ActivityLog> getPage(int page, int size) {
        int p = Math.max(0, page);
        int s = Math.max(1, Math.min(size, 50));
        Pageable pageable = PageRequest.of(p, s);
        return repository.findRecent(pageable);
    }
}
