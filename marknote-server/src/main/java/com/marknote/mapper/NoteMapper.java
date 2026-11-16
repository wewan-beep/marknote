package com.marknote.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.marknote.entity.Note;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NoteMapper extends BaseMapper<Note> {
}
