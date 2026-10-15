// Copyright 2026 Karma Krafts
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

#pragma once

#if defined(__i386__) || defined(_M_IX86)
#define FILAMENT_I386
#elif defined(__x86_64__) || defined(_M_X64)
#define FILAMENT_AMD64
#elif defined(__aarch64__) || defined(_M_ARM64)
#define FILAMENT_ARM64
#elif defined(__arm__) || defined(_M_ARM)
#define FILAMENT_ARM32
#else
#error "Unsupported target platform"
#endif