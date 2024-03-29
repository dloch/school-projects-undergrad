#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>
#include <sys/time.h>
#include "common.h"

pthread_mutex_t *switch_mutex;

pthread_t *child_thread;

int *buffer;
long int bufferTime;

void read()
{
	int counter;
	struct timeval start, end;
	gettimeofday(&start, NULL);
	for(counter = 0; counter < ARR_SIZE; counter += STRIDE/sizeof(int))
		buffer[counter]++;
	gettimeofday(&end, NULL);
	bufferTime += get_time_diff(start, end);
}

void *run_thread()
{
	int counter;
	for(counter = 0; counter < NUM_RUNS; counter++)
	{
		pthread_mutex_lock(switch_mutex);
		read();
		pthread_mutex_unlock(switch_mutex);
	}
	return NULL;
}

int main(int argc, char **argv)
{
	struct timeval start, end;
	buffer = (int*)malloc(ARR_SIZE*sizeof(int));
	switch_mutex = (pthread_mutex_t*)malloc(sizeof(pthread_mutex_t));

	pthread_mutex_init(switch_mutex, NULL);

	child_thread = (pthread_t*)malloc(sizeof(pthread_t));

	pthread_create(child_thread, NULL, &run_thread, NULL);

	gettimeofday(&start, NULL);

	run_thread();

	gettimeofday(&end, NULL);

	pthread_join(*child_thread, NULL);

	printf("Total time: %ld\n", get_time_diff(start, end));
	printf("Time reading buffer: %ld\n", bufferTime);

	return 0;
}
