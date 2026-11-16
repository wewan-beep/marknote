package com.marknote.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.marknote.entity.NoteHistory;
import com.marknote.mapper.NoteHistoryMapper;
import com.marknote.service.NoteHistoryService;
import org.springframework.stereotype.Service;

@Service
public class NoteHistoryServiceImpl extends ServiceImpl<NoteHistoryMapper, NoteHistory>
        implements NoteHistoryService {
}
