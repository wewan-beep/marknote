package com.marknote.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.marknote.entity.Note;
import com.marknote.entity.NoteHistory;
import com.marknote.mapper.NoteMapper;
import com.marknote.service.NoteHistoryService;
import com.marknote.service.NoteService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoteServiceImpl extends ServiceImpl<NoteMapper, Note> implements NoteService {

    @Resource
    private NoteHistoryService noteHistoryService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNoteWithHistory(Note note) {
        Note old = getById(note.getId());
        if (old == null) {
            throw new IllegalArgumentException("笔记不存在");
        }

        // 1. 保存旧内容为历史快照
        NoteHistory history = new NoteHistory();
        history.setNoteId(old.getId());
        history.setUserId(old.getUserId());
        history.setTitle(old.getTitle());
        history.setContent(old.getContent());
        noteHistoryService.save(history);

        // 2. 更新笔记，版本号 +1
        note.setUserId(null); // 不允许改归属
        note.setVersion((old.getVersion() == null ? 1 : old.getVersion()) + 1);
        note.setUpdateTime(null);
        updateById(note);
    }
}
