package main

import (
	"context"
	"errors"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/koreicc/garou-second-brain/backend/internal/config"
	"github.com/koreicc/garou-second-brain/backend/internal/handler"
	"github.com/koreicc/garou-second-brain/backend/internal/vault"
	"github.com/labstack/echo/v4"
	"github.com/labstack/echo/v4/middleware"
)

func main() {
	cfg := config.Load()

	v := vault.New(cfg.VaultPath)
	if err := v.Init(); err != nil {
		log.Fatalf("Failed to initialize vault: %v", err)
	}

	log.Printf("Vault initialized at: %s", cfg.VaultPath)

	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	e := echo.New()

	e.Use(middleware.Logger())
	e.Use(middleware.Recover())
	e.Use(middleware.CORS())
	e.Use(func(next echo.HandlerFunc) echo.HandlerFunc {
		return func(c echo.Context) error {
			c.Response().Header().Set("Content-Type", "application/json")
			return next(c)
		}
	})

	api := e.Group("/api/v1")

	noteHandler := handler.NewNoteHandler(v)
	api.GET("/notes", noteHandler.List)
	api.GET("/notes/:id", noteHandler.Get)
	api.POST("/notes", noteHandler.Create)
	api.PUT("/notes/:id", noteHandler.Update)
	api.DELETE("/notes/:id", noteHandler.Delete)

	taskHandler := handler.NewTaskHandler(v)
	api.GET("/tasks", taskHandler.List)
	api.GET("/tasks/:id", taskHandler.Get)
	api.POST("/tasks", taskHandler.Create)
	api.PUT("/tasks/:id", taskHandler.Update)
	api.DELETE("/tasks/:id", taskHandler.Delete)

	qtHandler := handler.NewQuickTaskHandlerWithContext(v, ctx)
	api.GET("/quick-tasks", qtHandler.List)
	api.POST("/quick-tasks", qtHandler.Create)
	api.PUT("/quick-tasks/:id/complete", qtHandler.MarkComplete)
	api.DELETE("/quick-tasks/:id", qtHandler.Delete)

	personHandler := handler.NewPersonHandler(v)
	api.GET("/people", personHandler.List)
	api.GET("/people/:id", personHandler.Get)
	api.POST("/people", personHandler.Create)
	api.PUT("/people/:id", personHandler.Update)
	api.DELETE("/people/:id", personHandler.Delete)

	searchHandler := handler.NewSearchHandler(v)
	api.GET("/search", searchHandler.Search)

	api.GET("/health", func(c echo.Context) error {
		return c.JSON(200, map[string]string{"status": "ok"})
	})

	// Graceful shutdown: when the process receives a termination signal, stop
	// accepting new requests. Cancelling ctx also stops pending quick-task
	// auto-delete timers.
	go func() {
		<-ctx.Done()
		log.Println("Shutting down server...")
		shutdownCtx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer cancel()
		if err := e.Shutdown(shutdownCtx); err != nil {
			log.Printf("Server shutdown error: %v", err)
		}
	}()

	addr := ":" + cfg.Port
	log.Printf("Server starting on %s", addr)

	srv := &http.Server{
		Addr:              addr,
		ReadTimeout:       15 * time.Second,
		WriteTimeout:      15 * time.Second,
		IdleTimeout:       60 * time.Second,
		ReadHeaderTimeout: 10 * time.Second,
	}
	if err := e.StartServer(srv); err != nil && !errors.Is(err, http.ErrServerClosed) {
		log.Fatalf("Server error: %v", err)
	}
	stop()
}
