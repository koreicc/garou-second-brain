package config

import (
	"errors"
	"fmt"
	"os"
	"path/filepath"
	"strconv"
)

type Config struct {
	VaultPath string
	Port      string
}

const (
	DefaultVaultPath = "~/second-brain/vault"
	DefaultPort      = "8080"
)

func Load() *Config {
	cfg := &Config{
		VaultPath: getEnv("SECOND_BRAIN_VAULT_PATH", DefaultVaultPath),
		Port:      getEnv("SECOND_BRAIN_PORT", DefaultPort),
	}

	if len(cfg.VaultPath) > 0 && cfg.VaultPath[0] == '~' {
		home, err := os.UserHomeDir()
		if err == nil {
			cfg.VaultPath = filepath.Join(home, cfg.VaultPath[1:])
		}
	}

	return cfg
}

// Validate checks that the configuration is valid and returns any errors.
func (c *Config) Validate() error {
	var errs []error

	// Validate vault path
	if c.VaultPath == "" {
		errs = append(errs, errors.New("vault path is empty"))
	} else {
		// Check if path is absolute after ~ expansion
		if !filepath.IsAbs(c.VaultPath) {
			errs = append(errs, fmt.Errorf("vault path must be absolute after ~ expansion: %s", c.VaultPath))
		}

		// Check if parent directory exists or can be created
		parent := filepath.Dir(c.VaultPath)
		if _, err := os.Stat(parent); err != nil && !os.IsNotExist(err) {
			errs = append(errs, fmt.Errorf("cannot access vault parent directory %s: %w", parent, err))
		}
	}

	// Validate port
	if c.Port == "" {
		errs = append(errs, errors.New("port is empty"))
	} else {
		portNum, err := strconv.Atoi(c.Port)
		if err != nil {
			errs = append(errs, fmt.Errorf("invalid port number: %s", c.Port))
		} else if portNum < 1 || portNum > 65535 {
			errs = append(errs, fmt.Errorf("port out of range (1-65535): %d", portNum))
		} else if portNum < 1024 {
			errs = append(errs, fmt.Errorf("port %d requires privileged access, use port >= 1024", portNum))
		}
	}

	if len(errs) > 0 {
		return fmt.Errorf("config validation failed: %w", errors.Join(errs...))
	}
	return nil
}

func getEnv(key, fallback string) string {
	if val, ok := os.LookupEnv(key); ok && val != "" {
		return val
	}
	return fallback
}
