/*
 * Copyright 2000-2007 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * @author max
 */
package com.intellij.util.concurrency;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DefaultReentrantReadWriteLockAdapter implements JBReentrantReadWriteLock {
  private final DefaultLockAdapter myReadLock;
  private final DefaultLockAdapter myWriteLock;
  private final ReentrantReadWriteLock myAdaptee;

  public DefaultReentrantReadWriteLockAdapter() {
    myAdaptee = new ReentrantReadWriteLock();
    myReadLock = new DefaultLockAdapter(myAdaptee.readLock());
    myWriteLock = new DefaultLockAdapter(myAdaptee.writeLock());
  }

  public JBLock readLock() {
    return myReadLock;
  }

  public JBLock writeLock() {
    return myWriteLock;
  }

  public boolean isWriteLockedByCurrentThread() {
    return myAdaptee.isWriteLockedByCurrentThread();
  }
}