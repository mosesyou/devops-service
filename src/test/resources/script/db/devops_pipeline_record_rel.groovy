package script.db

databaseChangeLog(logicalFilePath: 'dba/devops_pipeline_record_rel.groovy') {
    changeSet(author: 'wanghao', id: '2020-07-14-create-table') {
        createTable(tableName: "devops_pipeline_record_rel", remarks: '流水线记录关系表') {
            column(name: 'id', type: 'BIGINT UNSIGNED', remarks: '主键，ID', autoIncrement: true) {
                constraints(primaryKey: true)
            }
            column(name: 'pipeline_id', type: 'BIGINT UNSIGNED', remarks: '流水线Id') {
                constraints(nullable: false)
            }
            column(name: 'ci_pipeline_record_id', type: 'BIGINT UNSIGNED', remarks: 'ci流水线Id') {
                constraints(nullable: false)
            }
            column(name: 'cd_pipeline_record_id', type: 'BIGINT UNSIGNED', remarks: 'cd流水线Id') {
                constraints(nullable: false)
            }
            column(name: "object_version_number", type: "BIGINT UNSIGNED", defaultValue: "1")
            column(name: "created_by", type: "BIGINT UNSIGNED", defaultValue: "0")
            column(name: "creation_date", type: "DATETIME", defaultValueComputed: "CURRENT_TIMESTAMP")
            column(name: "last_updated_by", type: "BIGINT UNSIGNED", defaultValue: "0")
            column(name: "last_update_date", type: "DATETIME", defaultValueComputed: "CURRENT_TIMESTAMP")
        }
        addUniqueConstraint(tableName: 'devops_pipeline_record_rel',
                constraintName: 'uk_complex_id', columnNames: 'pipeline_id,ci_pipeline_record_id,cd_pipeline_record_id')
    }

}
