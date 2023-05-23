package ape.redis.pipes.generalPurpose

import ape.pipe.Pipe

trait RedisPipe[E, ZE, T1, T2] extends Pipe[E, ZE, T1, T2]
