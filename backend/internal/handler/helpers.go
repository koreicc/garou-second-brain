package handler

import (
	"errors"

	"github.com/koreicc/garou-second-brain/backend/internal/vault"
)

func isNotFound(err error) bool {
	return errors.Is(err, vault.ErrNotFound)
}
