// This file is licensed under the Elastic License 2.0. Copyright 2021-present, StarRocks Inc.

#pragma once

#include <string_view>
#include <utility>

#include "column/vectorized_field.h"
#include "column/vectorized_fwd.h"
#include "gen_cpp/olap_file.pb.h"

namespace starrocks::vectorized {

// TODO: move constructor and move assignment
class VectorizedSchema {
public:
    VectorizedSchema() = default;
    VectorizedSchema(VectorizedSchema&&) = default;
    VectorizedSchema& operator=(VectorizedSchema&&) = default;

#ifdef BE_TEST
    explicit VectorizedSchema(VectorizedFields fields);
#endif

    explicit VectorizedSchema(VectorizedFields fields, KeysType keys_type, std::vector<ColumnId> sort_key_idxes);

    // if we use this constructor and share the name_to_index with another schema,
    // we must make sure another shema is read only!!!
    explicit VectorizedSchema(VectorizedSchema* schema);

    explicit VectorizedSchema(VectorizedSchema* schema, const std::vector<ColumnId>& cids);

    // if we use this constructor and share the name_to_index with another schema,
    // we must make sure another shema is read only!!!
    VectorizedSchema(const VectorizedSchema& schema);

    // if we use this constructor and share the name_to_index with another schema,
    // we must make sure another shema is read only!!!
    VectorizedSchema& operator=(const VectorizedSchema& other);

    size_t num_fields() const { return _fields.size(); }

    size_t num_key_fields() const { return _num_keys; }

    const std::vector<ColumnId> sort_key_idxes() const { return _sort_key_idxes; }

    void reserve(size_t size) { _fields.reserve(size); }

    void append(const VectorizedFieldPtr& field);
    void insert(size_t idx, const VectorizedFieldPtr& field);
    void remove(size_t idx);

    void clear() {
        _fields.clear();
        _num_keys = 0;
        _name_to_index.reset();
        _name_to_index_append_buffer.reset();
        _share_name_to_index = false;
    }

    const VectorizedFieldPtr& field(size_t idx) const;
    const VectorizedFields& fields() const { return _fields; }

    std::vector<std::string> field_names() const;

    // return null if name not found
    VectorizedFieldPtr get_field_by_name(const std::string& name) const;

    size_t get_field_index_by_name(const std::string& name) const;

    void convert_to(VectorizedSchema* new_schema, const std::vector<LogicalType>& new_types) const;

    KeysType keys_type() const { return static_cast<KeysType>(_keys_type); }

private:
    void _build_index_map(const VectorizedFields& fields);

    VectorizedFields _fields;
    size_t _num_keys = 0;
    std::vector<ColumnId> _sort_key_idxes;
    std::shared_ptr<std::unordered_map<std::string_view, size_t>> _name_to_index;

    // If we share the same _name_to_index with another vectorized schema,
    // newly append(only append here) fields will be added directly to the
    // _name_to_index_append_buffer. Reasons why we use this: because we share
    // _name_to_index with another vectorized schema, we cannot directly append
    // the newly fields to the shared _name_to_index. One possible solution is
    // COW(copy on write), allocate a new _name_to_index and append the newly
    // fields to it. However, in our scenario, usually we only need to append
    // few fields(maybe only 1) to the new schema, there is not need to use COW
    // to allocate a new _name_to_index, so here we saves new append fileds in
    // _name_to_index_append_buffer.
    std::shared_ptr<std::unordered_map<std::string_view, size_t>> _name_to_index_append_buffer;
    // If _share_name_to_index is true, it means that we share the _name_to_index
    // with another schema, and we only perform read or append to current schema,
    // the append field's name will be written to _name_to_index_append_buffer.
    mutable bool _share_name_to_index = false;

    uint8_t _keys_type = static_cast<uint8_t>(DUP_KEYS);
};

inline std::ostream& operator<<(std::ostream& os, const VectorizedSchema& schema) {
    const VectorizedFields& fields = schema.fields();
    os << "(";
    if (!fields.empty()) {
        os << *fields[0];
    }
    for (size_t i = 1; i < fields.size(); i++) {
        os << ", " << *fields[i];
    }
    os << ")";
    return os;
}

} // namespace starrocks::vectorized
