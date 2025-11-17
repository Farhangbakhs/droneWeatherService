package cw1.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Region {

    @NotBlank
    private String name;

    @NotNull
    @Size(min = 4)
    @Valid
    private List<LngLat> vertices;

    public String getName() {
        return name;
    }

    public List<LngLat> getVertices() {
        return vertices;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setVertices(List<LngLat> vertices) {
        this.vertices = vertices;
    }

    @AssertTrue(message = "polygon must be closed")
    public boolean isClosed() {
        if (vertices == null || vertices.size() < 2) return false;
        LngLat first = vertices.get(0);
        LngLat last = vertices.get(vertices.size() - 1);
        return first != null && last != null
                && first.getLng() != null && first.getLat() != null
                && last.getLng() != null && last.getLat() != null
                && first.getLng().equals(last.getLng())
                && first.getLat().equals(last.getLat());
    }

    @AssertTrue(message = "need at least 3 distinct vertices")
    public boolean hasMinDistinct() {
        if (vertices == null || vertices.size() < 4) return false;
        Set<String> uniq = new HashSet<>();
        for (int i = 0; i < vertices.size() - 1; i++) {
            LngLat p = vertices.get(i);
            if (p == null || p.getLng() == null || p.getLat() == null) return false;
            uniq.add(p.getLng() + "," + p.getLat());
        }
        return uniq.size() >= 3;
    }
}