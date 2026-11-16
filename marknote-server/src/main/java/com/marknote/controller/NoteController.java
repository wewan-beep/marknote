package com.marknote.controller;

import com.marknote.entity.Note;
import com.marknote.service.NoteService;
import com.marknote.util.Result;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/note")
public class NoteController {

    @Resource
    private NoteService noteService;

    private Long uid(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    /**
     * 笔记列表（isDeleted 默认 0 正常，1 回收站）
     */
    @GetMapping("/list")
    public Result<List<Note>> list(@RequestParam(required = false, defaultValue = "0") Integer isDeleted,
                                   @RequestParam(required = false) String keyword,
                                   HttpServletRequest request) {
        List<Note> list = noteService.lambdaQuery()
                .eq(Note::getUserId, uid(request))
                .eq(Note::getIsDeleted, isDeleted)
                .like(keyword != null && !keyword.trim().isEmpty(), Note::getTitle, keyword == null ? "" : keyword.trim())
                .orderByDesc(Note::getUpdateTime)
                .list();
        return Result.success(list);
    }

    /**
     * 新建笔记
     */
    @PostMapping("/add")
    public Result<Note> add(@RequestBody Note note, HttpServletRequest request) {
        note.setUserId(uid(request));
        note.setIsDeleted(0);
        note.setVersion(1);
        if (note.getTitle() == null || note.getTitle().trim().isEmpty()) {
            note.setTitle("无标题笔记");
        }
        if (note.getContent() == null) {
            note.setContent("");
        }
        noteService.save(note);
        return Result.success(note);
    }

    /**
     * 更新笔记（自动生成历史快照）
     */
    @PostMapping("/update")
    public Result<Long> update(@RequestBody Note note, HttpServletRequest request) {
        Note old = noteService.getById(note.getId());
        if (old == null || !old.getUserId().equals(uid(request))) {
            return Result.fail("无权操作该笔记");
        }
        noteService.updateNoteWithHistory(note);
        Long newVersion = Long.valueOf(note.getVersion() == null ? null : note.getVersion());
        return Result.success(newVersion);
    }

    /**
     * 移入回收站
     */
    @PostMapping("/del")
    public Result<Boolean> del(@RequestBody Note note, HttpServletRequest request) {
        Note old = noteService.getById(note.getId());
        if (old == null || !old.getUserId().equals(uid(request))) {
            return Result.fail("无权操作该笔记");
        }
        Note upd = new Note();
        upd.setId(note.getId());
        upd.setIsDeleted(1);
        noteService.updateById(upd);
        return Result.success(true);
    }

    /**
     * 从回收站恢复
     */
    @PostMapping("/restore")
    public Result<Boolean> restore(@RequestBody Note note, HttpServletRequest request) {
        Note old = noteService.getById(note.getId());
        if (old == null || !old.getUserId().equals(uid(request))) {
            return Result.fail("无权操作该笔记");
        }
        Note upd = new Note();
        upd.setId(note.getId());
        upd.setIsDeleted(0);
        noteService.updateById(upd);
        return Result.success(true);
    }

    /**
     * 彻底删除
     */
    @PostMapping("/remove")
    public Result<Boolean> remove(@RequestBody Note note, HttpServletRequest request) {
        Note old = noteService.getById(note.getId());
        if (old == null || !old.getUserId().equals(uid(request))) {
            return Result.fail("无权操作该笔记");
        }
        noteService.removeById(note.getId());
        return Result.success(true);
    }

    /**
     * 笔记详情
     */
    @GetMapping("/get")
    public Result<Note> get(@RequestParam Long id, HttpServletRequest request) {
        Note note = noteService.getById(id);
        if (note == null || !note.getUserId().equals(uid(request))) {
            return Result.fail("笔记不存在");
        }
        return Result.success(note);
    }
}
