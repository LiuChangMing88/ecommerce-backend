package com.github.liuchangming88.ecommerce_backend.configuration.JpaDataPage;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

/*
  This class is here because Jackson (used by Redis as of now) can't deserialize PageImpl because there's no @JsonProperty annotation instructing it.
  This class is used in place of java's default pagination Page<T>.
 */

@JsonIgnoreProperties(ignoreUnknown = true, value = {"pageable", "sort"})  // Ignore complex fields if not needed
public class RestPage<T> extends PageImpl<T> {

    @JsonCreator
    public RestPage(@JsonProperty("content") List<T> content,
                    @JsonProperty("number") int number,
                    @JsonProperty("size") int size,
                    @JsonProperty("totalElements") long totalElements) {
        super(content, PageRequest.of(number, size), totalElements);
    }

    public RestPage(Pageable pageable, List<T> content, long total) {
        super(content, pageable, total);
    }
}