package com.marknote.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.marknote.entity.NoteHistory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NoteHistoryMapper extends BaseMapper<NoteHistory> {
}
