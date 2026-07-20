package handler

import (
	"errors"
	"strconv"

	"github.com/koreicc/garou-second-brain/backend/internal/vault"
	"github.com/labstack/echo/v4"
)

func isNotFound(err error) bool {
	return errors.Is(err, vault.ErrNotFound)
}

// queryParamInt returns the named query parameter as an int, or defaultValue if
// the parameter is missing or malformed.
func queryParamInt(c echo.Context, name string, defaultValue int) int {
	val := c.QueryParam(name)
	if val == "" {
		return defaultValue
	}
	i, err := strconv.Atoi(val)
	if err != nil {
		return defaultValue
	}
	return i
}

// paginate applies offset/limit pagination to a slice. offset and limit are
// read from query parameters "offset" and "limit". Defaults are 0 and 50.
// limit is capped at 100. The returned slice is a sub-slice of items.
func paginate[T any](items []T, c echo.Context) []T {
	offset := queryParamInt(c, "offset", 0)
	limit := queryParamInt(c, "limit", 50)

	if offset < 0 {
		offset = 0
	}
	if limit < 0 {
		limit = 0
	}
	if limit > 100 {
		limit = 100
	}

	if offset >= len(items) {
		return []T{}
	}

	end := offset + limit
	if end > len(items) {
		end = len(items)
	}

	return items[offset:end]
}
