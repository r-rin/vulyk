ALTER TABLE lab_flags
    RENAME COLUMN flag_value TO flag_hash;

ALTER TABLE lab_flags
    DROP CONSTRAINT uc_lab_flags_flag_value;

ALTER TABLE lab_flags
    ADD CONSTRAINT uc_lab_flags_flag_hash UNIQUE (flag_hash);