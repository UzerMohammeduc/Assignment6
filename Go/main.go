package main

import (
	"fmt"
	"os"
	"sync"
	"time"
)

// TaskQueue using buffered channel for concurrency-safe access
type TaskQueue struct {
	tasks chan string
}

func NewTaskQueue(size int) *TaskQueue {
	return &TaskQueue{tasks: make(chan string, size)}
}

func (q *TaskQueue) AddTask(task string) {
	q.tasks <- task
}

func (q *TaskQueue) GetTask() (string, bool) {
	task, ok := <-q.tasks
	return task, ok
}

func (q *TaskQueue) Close() {
	close(q.tasks)
}

func worker(id int, queue *TaskQueue, wg *sync.WaitGroup, mu *sync.Mutex, file *os.File) {
	defer wg.Done()
	fmt.Printf("[LOG] Worker %d started\n", id)

	for {
		task, ok := queue.GetTask()
		if !ok {
			break
		}
		time.Sleep(500 * time.Millisecond) // simulate work

		result := fmt.Sprintf("Processed by worker %d: %s\n", id, task)

		// Safe write to file
		mu.Lock()
		_, err := file.WriteString(result)
		mu.Unlock()

		if err != nil {
			fmt.Printf("[ERROR] Worker %d: %v\n", id, err)
		}
	}

	fmt.Printf("[LOG] Worker %d completed\n", id)
}

func main() {
	taskQueue := NewTaskQueue(20)

	// Add sample tasks
	for i := 1; i <= 20; i++ {
		taskQueue.AddTask(fmt.Sprintf("Task %d", i))
	}
	taskQueue.Close()

	// Create output file
	file, err := os.Create("output.txt")
	if err != nil {
		fmt.Println("Failed to create output.txt:", err)
		return
	}
	defer file.Close()

	var wg sync.WaitGroup
	var mu sync.Mutex
	numWorkers := 4

	for i := 1; i <= numWorkers; i++ {
		wg.Add(1)
		go worker(i, taskQueue, &wg, &mu, file)
	}

	wg.Wait()
	fmt.Println("All tasks completed. Results written to output.txt.")
}
