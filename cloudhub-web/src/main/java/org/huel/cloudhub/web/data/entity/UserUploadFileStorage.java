package org.huel.cloudhub.web.data.entity;

import space.lingu.light.DataColumn;
import space.lingu.light.DataTable;
import space.lingu.light.LightConfiguration;
import space.lingu.light.PrimaryKey;

/**
 * @author RollW
 */
@DataTable(tableName = "user_upload_table", configuration =
@LightConfiguration(key = LightConfiguration.KEY_VARCHAR_LENGTH, value = "120"))
public record UserUploadFileStorage(
        @DataColumn(name = "user_id")
        @PrimaryKey
        Long id,

        @DataColumn(name = "image_id")
        @PrimaryKey
        String imageId,

        @DataColumn(name = "image_file_size")
        Long fileSize
) {

}