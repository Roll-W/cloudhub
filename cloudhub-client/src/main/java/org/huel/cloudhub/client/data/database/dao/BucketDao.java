package org.huel.cloudhub.client.data.database.dao;

import org.huel.cloudhub.client.data.dto.bucket.BucketInfo;
import org.huel.cloudhub.client.data.dto.user.UserInfo;
import org.huel.cloudhub.client.data.entity.bucket.Bucket;
import org.huel.cloudhub.client.data.entity.user.Role;
import org.huel.cloudhub.client.data.entity.user.User;
import space.lingu.light.*;

import java.util.List;

/**
 * @author Cheng
 * 桶的name 相当于id
 * name, userId,createTime, bucketVisibility
 */
@Dao
public abstract class BucketDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    public abstract void insert(Bucket... buckets);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    public abstract void insert(List<Bucket> buckets);

    @Update(onConflict = OnConflictStrategy.ABORT)
    public abstract void update(Bucket... buckets);

    @Update(onConflict = OnConflictStrategy.ABORT)
    public abstract void update(List<Bucket> buckets);

    @Delete
    public abstract void delete(Bucket... buckets);

    @Transaction
    @Delete("DELETE FROM user_buckets_table WHERE bucket_name = {info.name()}")
    public abstract void delete(BucketInfo info);

    @Delete("DELETE FROM user_buckets_table WHERE bucket_name = {name}")
    public abstract void deleteByName(String name);

    @Delete("DELETE FROM user_buckets_table WHERE bucket_name IN {names}")
    public abstract void deleteByNames(List<Long> names);

    @Delete
    public abstract void delete(List<Bucket> buckets);

    //包含桶的名称和用户id的集合
    @Query("SELECT bucket_name, bucket_user_id FROM user_buckets_table")
    public abstract List<BucketInfo> bucketInfos();

//    根据名称查询一个桶对象
    @Query("SELECT * FROM user_buckets_table WHERE bucket_name = {name}")
    public abstract Bucket getAllBucketByName(String name);

//  根据用户id查询出桶的名称
    @Query("SELECT bucket_name FROM user_buckets_table WHERE bucket_user_id = {id}")
    public abstract String getBucketByUserId(String id);

//  根据桶的名称查询桶的名字 -.-
    @Query("SELECT user_name FROM user_table WHERE user_name = {name}")
    public abstract String getBucketByName(String name);

}
