package com.nowcoder.community.entity;

import lombok.Data;

/**
 * 封装分页相关的信息
 */
@Data
public class Page {
    // 当前页码
    private Integer current = 1;

    // 显示上限
    private Integer limit = 10;

    // 数据总数（用于计算总页数）
    private Integer rows;

    // 查询路径（用于复用分页链接）
    private String path;

    public void setCurrent(Integer current) {
        if (current >= 1) {
            this.current = current;
        }
    }

    public void setLimit(Integer limit) {
        if (limit >= 1 && limit <= 100) {
            this.limit = limit;
        }
    }

    public void setRows(Integer rows) {
        if (rows >= 0) {
            this.rows = rows;
        }
    }

    /**
     * 获取当前页的起始行
     *
     * @return
     */
    public Integer getOffset() {
        return (current - 1) * limit;
    }

    /**
     * 获取总页数
     *
     * @return
     */
    public Integer getTotal() {
        if (rows % limit == 0) {
            return rows / limit;
        } else {
            return rows / limit + 1;
        }
    }

    /**
     * 获取起始页码
     *
     * @return
     */
    public Integer getFrom() {
        return current + 2 > getTotal() ? Math.max(1, getTotal() - 5) : Math.max(current - 2, 1);
    }

    /**
     * 获取结束页码
     *
     * @return
     */
    public Integer getTo() {
        return current - 2 < 1 ? Math.min(5, getTotal()) : Math.min(current + 2, getTotal());
    }
}
