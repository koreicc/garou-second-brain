package model

import "fmt"

type ValidationError struct {
	Message string
}

func (e *ValidationError) Error() string {
	return e.Message
}

func NewValidationError(msg string) error {
	return &ValidationError{Message: msg}
}

func IsValidationError(err error) bool {
	_, ok := err.(*ValidationError)
	return ok
}

type ConflictError struct {
	Message string
}

func (e *ConflictError) Error() string {
	return e.Message
}

func NewConflictError(msg string) error {
	return &ConflictError{Message: msg}
}

func IsConflictError(err error) bool {
	_, ok := err.(*ConflictError)
	return ok
}

type NotFoundError struct {
	Message string
}

func (e *NotFoundError) Error() string {
	return e.Message
}

func NewNotFoundError(msg string) error {
	return &NotFoundError{Message: msg}
}

func IsNotFoundError(err error) bool {
	_, ok := err.(*NotFoundError)
	return ok
}

func WrapError(msg string, err error) error {
	return fmt.Errorf("%s: %w", msg, err)
}
