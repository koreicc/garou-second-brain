package config

import (
	"os"
	"path/filepath"
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

func getEnv(key, fallback string) string {
	if val, ok := os.LookupEnv(key); ok && val != "" {
		return val
	}
	return fallback
}
