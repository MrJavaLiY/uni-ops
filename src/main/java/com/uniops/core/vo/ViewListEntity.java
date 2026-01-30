package com.uniops.core.vo;

import lombok.Data;

import java.util.List;

/**
 * ViewListEntity 类的简要描述
 *
 * @author liyang
 * @since 2026/1/20
 */
@Data
public class ViewListEntity<T> {
    protected int page;
    protected int size;
    protected int totalPages;
    protected long total;
    protected List<T> records;
}
