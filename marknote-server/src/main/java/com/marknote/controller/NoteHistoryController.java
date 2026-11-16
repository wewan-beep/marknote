package com.marknote.controller;

import com.marknote.entity.NoteHistory;
import com.marknote.service.NoteHistoryService;
import com.marknote.util.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/noteHistory")
public class NoteHistoryController {

    @Resource
    private NoteHistoryService noteHistoryService;

    /**
     * 某个笔记的历史版本列表
     */
    @GetMapping("/list")
    public Result<List<NoteHistory>> list(@RequestParam Long noteId) {
        List<NoteHistory> list = noteHistoryService.lambdaQuery()
                .eq(NoteHistory::getNoteId, noteId)
                .orderByDesc(NoteHistory::getCreateTime)
                .last("limit 50")
                .list();
        return Result.success(list);
    }
}
