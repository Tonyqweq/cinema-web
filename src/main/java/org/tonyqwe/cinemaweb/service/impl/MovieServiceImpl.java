package org.tonyqwe.cinemaweb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.tonyqwe.cinemaweb.domain.dto.MovieBodyRequest;
import org.tonyqwe.cinemaweb.domain.dto.MovieImportResult;
import org.tonyqwe.cinemaweb.domain.entity.Movies;
import org.tonyqwe.cinemaweb.mapper.MovieMapper;
import org.tonyqwe.cinemaweb.service.MovieService;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class MovieServiceImpl extends ServiceImpl<MovieMapper, Movies> implements MovieService {

    @Resource
    private MovieMapper movieMapper;

    @Override
    public IPage<Movies> pageMovies(long page, long pageSize, String title, String language, String country, String sortBy, String sortOrder) {
        Page<Movies> mpPage = new Page<>(page, pageSize);

        LambdaQueryWrapper<Movies> qw = new LambdaQueryWrapper<>();
        if (title != null && !title.isBlank()) {
            qw.like(Movies::getTitle, title.trim());
        }
        if (language != null && !language.isBlank()) {
            qw.eq(Movies::getLanguage, language.trim());
        }
        if (country != null && !country.isBlank()) {
            qw.eq(Movies::getCountry, country.trim());
        }

        if (sortBy != null && !sortBy.isBlank()) {
            boolean asc = "asc".equalsIgnoreCase(sortOrder);

            if ("duration_min".equalsIgnoreCase(sortBy) || "durationMin".equalsIgnoreCase(sortBy)) {
                if (asc) qw.orderByAsc(Movies::getDurationMin);
                else qw.orderByDesc(Movies::getDurationMin);
            } else if ("release_date".equalsIgnoreCase(sortBy) || "releaseDate".equalsIgnoreCase(sortBy)) {
                if (asc) qw.orderByAsc(Movies::getReleaseDate);
                else qw.orderByDesc(Movies::getReleaseDate);
            }
        }

        return movieMapper.selectPage(mpPage, qw);
    }

    @Override
    public java.util.List<String> listLanguages() {
        return movieMapper.selectDistinctLanguages();
    }

    @Override
    public java.util.List<String> listCountries() {
        return movieMapper.selectDistinctCountries();
    }

    @Override
    public Movies getMovieById(Long id) {
        if (id == null) return null;
        return movieMapper.selectById(id);
    }

    @Override
    public Movies updateMovieStatus(Long id, Integer status) {
        if (id == null || status == null) return null;
        Movies movie = movieMapper.selectById(id);
        if (movie == null) return null;
        movie.setStatus(status);
        movieMapper.updateById(movie);
        return movie;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteMovie(Long id) {
        if (id == null) {
            return false;
        }
        if (movieMapper.selectById(id) == null) {
            return false;
        }
        return movieMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Movies createMovie(MovieBodyRequest request) {
        Date now = new Date();
        Movies movie = new Movies();
        applyWritableFields(movie, request);
        movie.setStatus(1);
        movie.setCreatedAt(now);
        movie.setUpdatedAt(now);
        movieMapper.insert(movie);
        return movieMapper.selectById(movie.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Movies updateMovieInfo(Long id, MovieBodyRequest request) {
        if (id == null) return null;
        Movies movie = movieMapper.selectById(id);
        if (movie == null) return null;
        applyWritableFields(movie, request);
        movie.setUpdatedAt(new Date());
        movieMapper.updateById(movie);
        return movieMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MovieImportResult importMoviesFromExcel(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请上传 Excel 文件");
        }
        String name = file.getOriginalFilename();
        if (name == null) {
            throw new IllegalArgumentException("文件名无效");
        }
        String lower = name.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(".xlsx") && !lower.endsWith(".xls")) {
            throw new IllegalArgumentException("仅支持 .xlsx 或 .xls");
        }

        MovieImportResult result = new MovieImportResult();
        try (InputStream in = file.getInputStream(); Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getNumberOfSheets() > 0 ? wb.getSheetAt(0) : null;
            if (sheet == null) {
                throw new IllegalArgumentException("工作簿无工作表");
            }
            DataFormatter fmt = new DataFormatter();
            int first = sheet.getFirstRowNum();
            Row firstRow = sheet.getRow(first);
            int dataStart;
            Map<String, Integer> col;
            if (isTitleHeaderRow(firstRow, fmt)) {
                col = parseHeaderRow(firstRow, fmt);
                dataStart = first + 1;
            } else {
                col = defaultMovieColumnMap();
                dataStart = first;
            }

            int last = sheet.getLastRowNum();
            for (int r = dataStart; r <= last; r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                String title = cellString(row, col.get("title"), fmt);
                if (title == null || title.isBlank()) {
                    continue;
                }
                int excelRow = r + 1;
                try {
                    MovieBodyRequest req = buildRequestFromRow(row, col, fmt);
                    if (req.getTitle() == null || req.getTitle().isBlank()) {
                        continue;
                    }
                    createMovie(req);
                    result.setSuccessCount(result.getSuccessCount() + 1);
                } catch (Exception ex) {
                    result.setFailCount(result.getFailCount() + 1);
                    String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
                    if (result.getErrors().size() < 30) {
                        result.getErrors().add("第 " + excelRow + " 行: " + msg);
                    }
                }
            }
        }
        return result;
    }

    private void applyWritableFields(Movies movie, MovieBodyRequest request) {
        movie.setTitle(request.getTitle().trim());
        movie.setOriginalTitle(trimToNull(request.getOriginalTitle()));
        movie.setLanguage(trimToNull(request.getLanguage()));
        movie.setCountry(trimToNull(request.getCountry()));
        movie.setDurationMin(request.getDurationMin());
        movie.setReleaseDate(parseReleaseDateOrNull(request.getReleaseDate()));
        movie.setDescription(trimToNull(request.getDescription()));
        movie.setPosterUrl(trimToNull(request.getPosterUrl()));
        movie.setTrailerUrl(trimToNull(request.getTrailerUrl()));
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static Date parseReleaseDateOrNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            LocalDate ld = LocalDate.parse(s.trim());
            return Date.from(ld.atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("上映日期格式应为 yyyy-MM-dd");
        }
    }

    private static boolean isTitleHeaderRow(Row row, DataFormatter fmt) {
        if (row == null) {
            return false;
        }
        Cell c = row.getCell(0);
        if (c == null) {
            return false;
        }
        String v = fmt.formatCellValue(c).trim();
        return "title".equalsIgnoreCase(v);
    }

    private static Map<String, Integer> parseHeaderRow(Row row, DataFormatter fmt) {
        Map<String, Integer> map = new HashMap<>();
        short last = row.getLastCellNum();
        for (int i = 0; i < last; i++) {
            Cell cell = row.getCell(i);
            if (cell == null) {
                continue;
            }
            String key = normalizeHeaderKey(fmt.formatCellValue(cell));
            if (!key.isEmpty()) {
                map.putIfAbsent(key, i);
            }
        }
        return map;
    }

    private static String normalizeHeaderKey(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    private static Map<String, Integer> defaultMovieColumnMap() {
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("title", 0);
        m.put("original_title", 1);
        m.put("language", 2);
        m.put("country", 3);
        m.put("duration_min", 4);
        m.put("release_date", 5);
        m.put("description", 6);
        m.put("poster_url", 7);
        m.put("trailer_url", 8);
        return m;
    }

    private static String cellString(Row row, Integer colIdx, DataFormatter fmt) {
        if (colIdx == null || row == null) {
            return null;
        }
        Cell c = row.getCell(colIdx);
        if (c == null) {
            return null;
        }
        String v = fmt.formatCellValue(c).trim();
        return v.isEmpty() ? null : v;
    }

    private MovieBodyRequest buildRequestFromRow(Row row, Map<String, Integer> col, DataFormatter fmt) {
        MovieBodyRequest req = new MovieBodyRequest();
        String title = cellString(row, col.get("title"), fmt);
        req.setTitle(title != null ? title : "");
        req.setOriginalTitle(cellString(row, col.get("original_title"), fmt));
        req.setLanguage(cellString(row, col.get("language"), fmt));
        req.setCountry(cellString(row, col.get("country"), fmt));
        req.setDurationMin(readDuration(row, col.get("duration_min"), fmt));
        req.setReleaseDate(readReleaseDateString(row, col.get("release_date"), fmt));
        req.setDescription(cellString(row, col.get("description"), fmt));
        req.setPosterUrl(cellString(row, col.get("poster_url"), fmt));
        req.setTrailerUrl(cellString(row, col.get("trailer_url"), fmt));
        return req;
    }

    private static Integer readDuration(Row row, Integer colIdx, DataFormatter fmt) {
        if (colIdx == null || row == null) {
            return null;
        }
        Cell c = row.getCell(colIdx);
        if (c == null) {
            return null;
        }
        try {
            if (c.getCellType() == CellType.NUMERIC) {
                return (int) Math.round(c.getNumericCellValue());
            }
            String s = fmt.formatCellValue(c).trim();
            if (s.isEmpty()) {
                return null;
            }
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String readReleaseDateString(Row row, Integer colIdx, DataFormatter fmt) {
        if (colIdx == null || row == null) {
            return null;
        }
        Cell c = row.getCell(colIdx);
        if (c == null) {
            return null;
        }
        if (c.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(c)) {
            Date d = c.getDateCellValue();
            LocalDate ld = d.toInstant().atZone(ZoneId.of("Asia/Shanghai")).toLocalDate();
            return ld.toString();
        }
        String s = fmt.formatCellValue(c).trim();
        return s.isEmpty() ? null : s;
    }
}

