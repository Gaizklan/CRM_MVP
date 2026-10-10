package org.example.crm.common.application;

import java.util.List;
import java.util.function.Function;

/** Página de resultados. */
public record PageResult<T>(List<T> content, int page, int size, long totalElements) {

  public PageResult {
    content = List.copyOf(content);
  }

  public int totalPages() {
    return size == 0 ? 0 : (int) ((totalElements + size - 1) / size);
  }

  public <R> PageResult<R> map(Function<T, R> mapper) {
    return new PageResult<>(content.stream().map(mapper).toList(), page, size, totalElements);
  }
}
