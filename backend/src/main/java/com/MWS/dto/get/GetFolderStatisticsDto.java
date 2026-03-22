package com.MWS.dto.get;

import com.MWS.model.File;

import java.util.List;

public record GetFolderStatisticsDto(
        int filesQuantity,
        long size,
        List<File> recentlyAddedFiles
) {
}
