package com.algoviz.mapper;

import com.algoviz.entity.FileStorage;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface FileStorageMapper {
    @Insert("INSERT INTO file_storage (id, file_name, original_name, file_type, file_size, file_path, storage_type, bucket_name, download_url, uploader_id, uploader_name, create_time) " +
            "VALUES (#{id}, #{fileName}, #{originalName}, #{fileType}, #{fileSize}, #{filePath}, #{storageType}, #{bucketName}, #{downloadUrl}, #{uploaderId}, #{uploaderName}, #{createTime})")
    int insert(FileStorage fileStorage);

    @Select("SELECT * FROM file_storage ORDER BY create_time DESC LIMIT #{limit} OFFSET #{offset}")
    List<FileStorage> findByPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM file_storage")
    int count();

    @Select("SELECT * FROM file_storage WHERE id = #{id}")
    FileStorage findById(String id);

    @Select("SELECT * FROM file_storage WHERE file_name = #{fileName}")
    FileStorage findByFileName(String fileName);

    @Select("SELECT * FROM file_storage WHERE uploader_id = #{uploaderId} ORDER BY create_time DESC LIMIT #{limit}")
    List<FileStorage> findByUploader(String uploaderId, int limit);

    @Update("UPDATE file_storage SET download_url=#{downloadUrl} WHERE id=#{id}")
    int updateDownloadUrl(@Param("id") String id, @Param("downloadUrl") String downloadUrl);

    @Delete("DELETE FROM file_storage WHERE id = #{id}")
    int delete(String id);

    @Select("SELECT IFNULL(SUM(file_size), 0) FROM file_storage")
    Long sumFileSize();

    @Select("SELECT file_type, COUNT(*) as count, SUM(file_size) as size FROM file_storage GROUP BY file_type")
    List<FileStorageStats> getFileStats();

    public interface FileStorageStats {
        String getFileType();
        Integer getCount();
        Long getSize();
    }
}
