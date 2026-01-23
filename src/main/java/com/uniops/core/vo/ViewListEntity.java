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
    private int page;
    private int size;
    private int totalPages;
    private long total;
    private List<T> records;
}
