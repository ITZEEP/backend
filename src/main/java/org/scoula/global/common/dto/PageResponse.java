package org.scoula.global.common.dto;

import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 페이징 응답 DTO */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PageResponse<T> {

      private List<T> content;
      private int page;
      private int size;
      private long totalElements;
      private int totalPages;
      private boolean first;
      private boolean last;
      private boolean hasNext;
      private boolean hasPrevious;

      /** PageRequest와 데이터로부터 PageResponse 생성 */
      public static <T> PageResponse<T> of(
              List<T> content, PageRequest pageRequest, long totalElements) {
          int totalPages = (int) Math.ceil((double) totalElements / pageRequest.getSize());
          boolean first = pageRequest.getPage() == 1;
          boolean last = pageRequest.getPage() >= totalPages;
          boolean hasNext = pageRequest.getPage() < totalPages;
          boolean hasPrevious = pageRequest.getPage() > 1;

          return PageResponse.<T>builder()
                  .content(content)
                  .page(pageRequest.getPage())
                  .size(pageRequest.getSize())
                  .totalElements(totalElements)
                  .totalPages(totalPages)
                  .first(first)
                  .last(last)
                  .hasNext(hasNext)
                  .hasPrevious(hasPrevious)
                  .build();
      }

      /** Spring Data Page 객체로부터 PageResponse 생성 */
      public static <T> PageResponse<T> from(org.springframework.data.domain.Page<T> page) {
          return PageResponse.<T>builder()
                  .content(page.getContent())
                  .page(page.getNumber() + 1) // Spring Data는 0-based, 우리는 1-based
                  .size(page.getSize())
                  .totalElements(page.getTotalElements())
                  .totalPages(page.getTotalPages())
                  .first(page.isFirst())
                  .last(page.isLast())
                  .hasNext(page.hasNext())
                  .hasPrevious(page.hasPrevious())
                  .build();
      }

      /** 빈 페이지 응답 생성 */
      public static <T> PageResponse<T> empty(PageRequest pageRequest) {
          return of(List.of(), pageRequest, 0L);
      }
}
