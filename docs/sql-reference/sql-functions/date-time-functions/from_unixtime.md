# from_unixtime

## Description

Converts a UNIX timestamp into the required time format. The default format is `yyyy-MM-dd HH:mm:ss`. It also supports the formats in [date_format](./date_format.md).

Currently, `string_format` supports the following formats:

```plain text
%Y：Year  e.g.：2014，1900
%m：Month   e.g.：12，09
%d：Day  e.g.：11，01
%H：Hour  e.g.：23，01，12
%i：Minute  e.g.：05，11
%s：Second  e.g.：59，01
```

Other formats are invalid and NULL will be returned.

## Syntax

```Haskell
DATETIME|DATE FROM_UNIXTIME(INT unix_timestamp[, VARCHAR string_format])
```

## Parameters

- `unix_timestamp`: the UNIX timestamp you want to convert. It must be of the INT type. If the specified timestamp is less than 0 or greater than 2147483647, NULL will be returned. That is, the range for timestamp is `1970-01-01 00:00:00` to `2038-01-19 11:14:07`.

- `string_format`: the required time format.

## Return value

Returns a value of the DATETIME or DATE type. If `string_format` specifies the DATE format, a DATE value is returned.

If the timestamp exceeds the value range or if `string_format` is invalid, NULL will be returned.

## Examples

```plain text
MySQL > select from_unixtime(1196440219);
+---------------------------+
| from_unixtime(1196440219) |
+---------------------------+
| 2007-12-01 00:30:19       |
+---------------------------+

MySQL > select from_unixtime(1196440219, 'yyyy-MM-dd HH:mm:ss');
+--------------------------------------------------+
| from_unixtime(1196440219, 'yyyy-MM-dd HH:mm:ss') |
+--------------------------------------------------+
| 2007-12-01 00:30:19                              |
+--------------------------------------------------+

MySQL > select from_unixtime(1196440219, '%Y-%m-%d');
+-----------------------------------------+
| from_unixtime(1196440219, '%Y-%m-%d')   |
+-----------------------------------------+
| 2007-12-01                              |
+-----------------------------------------+

MySQL > select from_unixtime(1196440219, '%Y-%m-%d %H:%i:%s');
+--------------------------------------------------+
| from_unixtime(1196440219, '%Y-%m-%d %H:%i:%s')   |
+--------------------------------------------------+
| 2007-12-01 00:30:19                              |
+--------------------------------------------------+
```

## keyword

FROM_UNIXTIME,FROM,UNIXTIME
