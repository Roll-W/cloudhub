package org.huel.cloudhub.client.disk.domain.operatelog;

import org.huel.cloudhub.client.disk.domain.systembased.SystemResourceKind;
import space.lingu.light.DataColumn;
import space.lingu.light.DataTable;
import space.lingu.light.LightConfiguration;
import space.lingu.light.PrimaryKey;
import space.lingu.light.SQLDataType;

/**
 * @author RollW
 */
@DataTable(name = "operate_log")
public class OperationLog {
    @DataColumn(name = "id")
    @PrimaryKey(autoGenerate = true)
    private final Long id;

    @DataColumn(name = "operator")
    private final long operator;

    @DataColumn(name = "operate_resource_id")
    private final long operateResourceId;

    @DataColumn(name = "resource_kind")
    private final SystemResourceKind systemResourceKind;

    @DataColumn(name = "action")
    private final Action action;

    @DataColumn(name = "operate_type")
    private final long operateType;

    @DataColumn(name = "operate_time", dataType = SQLDataType.TIMESTAMP)
    private final long operateTime;

    @DataColumn(name = "address")
    private final String address;

    @DataColumn(name = "origin_content")
    @LightConfiguration(key = LightConfiguration.KEY_VARCHAR_LENGTH, value = "500")
    private final String originContent;

    @DataColumn(name = "changed_content")
    @LightConfiguration(key = LightConfiguration.KEY_VARCHAR_LENGTH, value = "500")
    private final String changedContent;

    public OperationLog(Long id, long operateResourceId,
                        long operator, long operateType, long operateTime,
                        String address, SystemResourceKind systemResourceKind,
                        Action action,
                        String originContent,
                        String changedContent) {
        this.id = id;
        this.operateResourceId = operateResourceId;
        this.operator = operator;
        this.operateType = operateType;
        this.operateTime = operateTime;
        this.address = address;
        this.systemResourceKind = systemResourceKind;
        this.action = action;
        this.originContent = originContent;
        this.changedContent = changedContent;
    }

    public Long getId() {
        return id;
    }

    public long getOperateResourceId() {
        return operateResourceId;
    }

    public long getOperator() {
        return operator;
    }

    public long getOperateType() {
        return operateType;
    }

    public long getOperateTime() {
        return operateTime;
    }

    public String getAddress() {
        return address;
    }

    public SystemResourceKind getSystemResourceKind() {
        return systemResourceKind;
    }

    public Action getAction() {
        return action;
    }

    public String getOriginContent() {
        return originContent;
    }

    public String getChangedContent() {
        return changedContent;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long id;
        private long operateResourceId;
        private long operator;
        private long operateType;
        private long operateTime;
        private String address;
        private SystemResourceKind systemResourceKind;
        private Action action;
        private String originContent;
        private String changedContent;

        public Builder() {

        }

        public Builder(OperationLog operationLog) {
            this.id = operationLog.id;
            this.operateResourceId = operationLog.operateResourceId;
            this.operator = operationLog.operator;
            this.operateType = operationLog.operateType;
            this.operateTime = operationLog.operateTime;
            this.address = operationLog.address;
            this.systemResourceKind = operationLog.systemResourceKind;
            this.action = operationLog.action;
            this.originContent = operationLog.originContent;
            this.changedContent = operationLog.changedContent;
        }

        public Builder setId(Long id) {
            this.id = id;
            return this;
        }

        public Builder setOperateResourceId(long operateResourceId) {
            this.operateResourceId = operateResourceId;
            return this;
        }

        public Builder setOperator(long operator) {
            this.operator = operator;
            return this;
        }

        public Builder setOperateTime(long operateTime) {
            this.operateTime = operateTime;
            return this;
        }

        public Builder setAddress(String address) {
            this.address = address;
            return this;
        }

        public Builder setSystemResourceKind(SystemResourceKind systemResourceKind) {
            this.systemResourceKind = systemResourceKind;
            return this;
        }

        public Builder setAction(Action action) {
            this.action = action;
            return this;
        }

        public Builder setOriginContent(String originContent) {
            this.originContent = originContent;
            return this;
        }

        public Builder setChangedContent(String changedContent) {
            this.changedContent = changedContent;
            return this;
        }

        public OperationLog build() {
            return new OperationLog(
                    id, operateResourceId,
                    operator, operateType, operateTime, address,
                    systemResourceKind,
                    action,
                    originContent,
                    changedContent
            );
        }
    }
}