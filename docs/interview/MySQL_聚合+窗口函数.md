# MySQL 聚合 + 窗口函数

我这里用一个很简单的共享单车订单表来举例。

假设有一张表：

```sql
ride_order (
  order_id BIGINT,
  user_id BIGINT,
  bike_id BIGINT,
  city VARCHAR(50),
  area_id BIGINT,
  start_time DATETIME,
  end_time DATETIME,
  status VARCHAR(20),   -- CREATED, RIDING, FINISHED, ABNORMAL_END
  amount DECIMAL(10,2)
)
```

---

## 1）按城市统计已完成订单数

### 题目

统计每个城市已完成订单数。

```sql
SELECT city, COUNT(*) AS finished_order_count
FROM ride_order
WHERE status = 'FINISHED'
GROUP BY city;
```

### 你面试时怎么解释

先用 `WHERE` 过滤口径，再用 `GROUP BY city` 聚合，最后 `COUNT(*)` 统计订单数。

---

## 2）统计每个城市的总收入

```sql
SELECT city, SUM(amount) AS total_amount
FROM ride_order
WHERE status = 'FINISHED'
GROUP BY city;
```

### 要点

`SUM(amount)` 是聚合，依然先过滤已完成订单，避免把未完成单算进去。

---

## 3）统计每个城市平均骑行金额，并筛出平均金额大于 5 的城市

```sql
SELECT city, AVG(amount) AS avg_amount
FROM ride_order
WHERE status = 'FINISHED'
GROUP BY city
HAVING AVG(amount) > 5;
```

### 解释

`HAVING` 是对聚合结果再过滤，和 `WHERE` 不一样。

---

## 4）统计每个城市每天订单量

```sql
SELECT city, DATE(start_time) AS ride_date, COUNT(*) AS order_count
FROM ride_order
WHERE status = 'FINISHED'
GROUP BY city, DATE(start_time);
```

### 解释

这题很像业务分析 SQL。
分组维度从一个变成两个：城市 + 日期。

---

## 5）每辆车最近一次骑行记录

这题就是窗口函数的经典场景。

```sql
SELECT *
FROM (
  SELECT
    order_id,
    bike_id,
    city,
    end_time,
    ROW_NUMBER() OVER (PARTITION BY bike_id ORDER BY end_time DESC) AS rn
  FROM ride_order
  WHERE status = 'FINISHED'
) t
WHERE t.rn = 1;
```

### 面试解释模板

如果题目是“每个分组取最新一条”，我会优先想到 `ROW_NUMBER()`。
这里 `PARTITION BY bike_id` 表示按车分组，`ORDER BY end_time DESC` 表示组内按结束时间倒序，`rn = 1` 就是最近一条。

---

## 6）每个城市订单量排名前 3 的区域

```sql
SELECT *
FROM (
  SELECT
    city,
    area_id,
    COUNT(*) AS order_count,
    RANK() OVER (PARTITION BY city ORDER BY COUNT(*) DESC) AS rk
  FROM ride_order
  WHERE status = 'FINISHED'
  GROUP BY city, area_id
) t
WHERE t.rk <= 3;
```

### 解释

先按 `city + area_id` 聚合，再在每个城市内部做排名。
这题能体现你知道“聚合 + 窗口函数”可以连着用。

---

## 7）看某辆车本次骑行和上一次骑行结束时间差

这个是 `LAG()` 的简单例子。

```sql
SELECT
  bike_id,
  order_id,
  end_time,
  LAG(end_time) OVER (PARTITION BY bike_id ORDER BY end_time) AS prev_end_time
FROM ride_order
WHERE status = 'FINISHED';
```

### 解释

`LAG()` 可以取当前行上一条记录的值。
如果面试官只是“听没听过窗口函数”，你说到 `ROW_NUMBER`、`RANK`、`LAG` 基本就够用了。

---

## 8）你面试时可以这样统一总结

> MySQL 聚合我一般先明确业务统计口径，再决定用 `GROUP BY`、聚合函数和 `HAVING`。
> 如果题目变成“分组后还要保留明细、排序、取最新一条或者做组内排名”，我会优先想到窗口函数，比如 `ROW_NUMBER()`、`RANK()`、`LAG()`。
> 对我来说，重点不是背函数，而是先想清楚：这个 SQL 要回答的业务问题到底是什么。
