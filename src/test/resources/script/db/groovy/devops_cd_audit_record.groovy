package script.db.groovy

databaseChangeLog(logicalFilePath: 'dba/devops_cd_audit_record.groovy') {
    changeSet(author: 'scp', id: '2020-06-29-create-table') {
        createTable(tableName: "devops_cd_audit_record", remarks: '执行关系记录') {
            column(name: 'id', type: 'BIGINT UNSIGNED', remarks: '主键，ID', autoIncrement: true) {
                constraints(primaryKey: true)
            }
            column(name: 'user_id', type: 'BIGINT UNSIGNED', remarks: '用户Id')
            column(name: 'pipeline_record_id', type: 'BIGINT UNSIGNED', remarks: '流水线记录Id')
            column(name: 'stage_record_id', type: 'BIGINT UNSIGNED', remarks: '阶段记录Id')
            column(name: 'job_record_id', type: 'BIGINT UNSIGNED', remarks: '任务记录Id')
            column(name: 'status', type: 'VARCHAR(64)', remarks: '人工审核的结果（拒绝或通过）')

            column(name: "object_version_number", type: "BIGINT UNSIGNED", defaultValue: "1")
            column(name: "created_by", type: "BIGINT UNSIGNED", defaultValue: "0")
            column(name: "creation_date", type: "DATETIME", defaultValueComputed: "CURRENT_TIMESTAMP")
            column(name: "last_updated_by", type: "BIGINT UNSIGNED", defaultValue: "0")
            column(name: "last_update_date", type: "DATETIME", defaultValueComputed: "CURRENT_TIMESTAMP")
        }
    }
}