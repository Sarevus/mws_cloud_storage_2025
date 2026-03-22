package com.MWS.dto;

import com.MWS.dto.create_update.FileDto;

import java.util.List;

public record FolderContextDto(
        List<FolderDto> subfolders,
        List<FileDto> allFiles
) {

}
