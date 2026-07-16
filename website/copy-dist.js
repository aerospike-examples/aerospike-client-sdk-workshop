import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const TARGETS = {
  spring: path.join(__dirname, '..', 'spring-server', 'src', 'main', 'resources', 'static'),
  python: path.join(__dirname, '..', 'python-server', 'static'),
  rust: path.join(__dirname, '..', 'rust-server', 'static'),
  both: null,
  all: null,
};

const distPath = path.join(__dirname, 'dist');

function copyDirectory(src, dest) {
  try {
    if (!fs.existsSync(dest)) {
      fs.mkdirSync(dest, { recursive: true });
    }

    const items = fs.readdirSync(src);

    for (const item of items) {
      const srcPath = path.join(src, item);
      const destPath = path.join(dest, item);
      const stat = fs.statSync(srcPath);

      if (stat.isDirectory()) {
        copyDirectory(srcPath, destPath);
      } else {
        fs.copyFileSync(srcPath, destPath);
      }
    }

    return true;
  } catch (error) {
    console.error('Error copying directory:', error);
    return false;
  }
}

function cleanDirectory(dir) {
  try {
    if (fs.existsSync(dir)) {
      fs.rmSync(dir, { recursive: true, force: true });
    }
    return true;
  } catch (error) {
    console.error('Error cleaning directory:', error);
    return false;
  }
}

function getAllFiles(dir) {
  let files = [];
  try {
    const items = fs.readdirSync(dir);
    for (const item of items) {
      const fullPath = path.join(dir, item);
      const stat = fs.statSync(fullPath);
      if (stat.isDirectory()) {
        files = files.concat(getAllFiles(fullPath));
      } else {
        files.push(fullPath);
      }
    }
  } catch {
    // Ignore errors
  }
  return files;
}

function copyDistToTarget(label, destinationPath) {
  console.log(`🔄 Copying built React app to ${label}...`);

  if (!fs.existsSync(distPath)) {
    console.error('❌ Error: dist directory not found. Please run "npm run build-only" first.');
    process.exit(1);
  }

  console.log('🧹 Cleaning destination directory...');
  if (!cleanDirectory(destinationPath)) {
    console.error(`❌ Failed to clean destination directory: ${destinationPath}`);
    process.exit(1);
  }

  console.log('📋 Copying files...');
  if (!copyDirectory(distPath, destinationPath)) {
    console.error(`❌ Failed to copy files to ${destinationPath}`);
    process.exit(1);
  }

  const files = getAllFiles(destinationPath);
  console.log(`✅ Successfully copied React build to ${label}!`);
  console.log(`📁 Files copied to: ${destinationPath}`);
  console.log(`📊 Copied ${files.length} files total`);
}

function resolveTargets() {
  const arg = process.argv[2] || 'all';
  if (arg === 'both') {
    return [
      ['Spring Boot resources', TARGETS.spring],
      ['Python server static', TARGETS.python],
    ];
  }
  if (arg === 'all') {
    return [
      ['Spring Boot resources', TARGETS.spring],
      ['Python server static', TARGETS.python],
      ['Rust server static', TARGETS.rust],
    ];
  }
  const destination = TARGETS[arg];
  if (!destination) {
    console.error(`❌ Unknown target "${arg}". Use: spring, python, rust, both, or all`);
    process.exit(1);
  }
  const labels = {
    spring: 'Spring Boot resources',
    python: 'Python server static',
    rust: 'Rust server static',
  };
  return [[labels[arg] || arg, destination]];
}

for (const [label, destination] of resolveTargets()) {
  copyDistToTarget(label, destination);
}
