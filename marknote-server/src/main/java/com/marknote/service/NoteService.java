package com.marknote.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.marknote.entity.Note;

public interface NoteService extends IService<Note> {

    /**
     * 更新笔记并写入历史快照
     */
    void updateNoteWithHistory(Note note);
}
