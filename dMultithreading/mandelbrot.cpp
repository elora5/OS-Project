// mandelbrot.cpp
// display mandelbrot.ppm


#include <cmath>
#include <cstdint>
#include <fstream>
#include <iostream>
#include <string>
#include <thread>
#include <vector>

static inline int mandelbrotIterations(double cr, double ci, int max_iters) {
    double zr = 0.0, zi = 0.0;
    int iter = 0;
    // Iterate until |z|^2 > 4 (i.e., |z| > 2) or we hit the iteration cap
    while ((zr * zr + zi * zi) <= 4.0 && iter < max_iters) {
        double zr2 = zr * zr - zi * zi + cr;
        double zi2 = 2.0 * zr * zi + ci;
        zr = zr2;
        zi = zi2;
        ++iter;
    }
    return iter;
}

int main(int argc, char* argv[]) {
    if (argc != 9) {
        std::cerr << "Usage:\n"
                  << "  " << argv[0]
                  << " <img_width> <img_height> <xmin> <xmax> <ymin> <ymax> <max_iterations> <output_file>\n"
                  << "Example:\n  " << argv[0]
                  << " 800 600 -2.0 0.47 -1.12 1.12 1000 mandelbrot.ppm\n";
        return 1;
    }

    // Parse inputs
    const int width        = std::stoi(argv[1]);
    const int height       = std::stoi(argv[2]);
    const double xmin      = std::stod(argv[3]);
    const double xmax      = std::stod(argv[4]);
    const double ymin      = std::stod(argv[5]);
    const double ymax      = std::stod(argv[6]);
    const int max_iters    = std::stoi(argv[7]);
    const std::string out  = argv[8];

    // Basic validation
    if (width <= 0 || height <= 0 || max_iters <= 0) {
        std::cerr << "Error: width, height, and max_iterations must be positive.\n";
        return 2;
    }
    if (!(xmin < xmax) || !(ymin < ymax)) {
        std::cerr << "Error: require xmin < xmax and ymin < ymax.\n";
        return 3;
    }

    // Precompute scales (avoid division by zero if width/height == 1)
    const double sx = (width  > 1) ? (xmax - xmin) / double(width  - 1) : 0.0;
    const double sy = (height > 1) ? (ymax - ymin) / double(height - 1) : 0.0;

    // Output image buffer: RGB, 8-bit per channel
    std::vector<std::uint8_t> img(size_t(width) * size_t(height) * 3, 0);

    // Fixed number of threads as required
    const int NUM_THREADS = 8;

    auto worker = [&](int tid) {
        for (int row = tid; row < height; row += NUM_THREADS) {
            // Map image row to complex imaginary coordinate:
            // top row (row=0) corresponds to ci = ymax
            const double ci = ymax - row * sy;
            for (int col = 0; col < width; ++col) {
                const double cr = xmin + col * sx;

                const int iter = mandelbrotIterations(cr, ci, max_iters);

                // Grayscale: brighter = fewer iterations; black if inside set
                const std::uint8_t val = (iter >= max_iters)
                    ? 0
                    : static_cast<std::uint8_t>(255.0 - (255.0 * iter / double(max_iters)));

                const size_t idx = (size_t(row) * size_t(width) + size_t(col)) * 3;
                img[idx + 0] = val; // R
                img[idx + 1] = val; // G
                img[idx + 2] = val; // B
            }
        }
    };

    // Launch threads
    std::vector<std::thread> threads;
    threads.reserve(NUM_THREADS);
    for (int t = 0; t < NUM_THREADS; ++t) {
        threads.emplace_back(worker, t);
    }
    for (auto& th : threads) th.join();

    // Write PPM (P6) file
    std::ofstream ofs(out, std::ios::binary);
    if (!ofs) {
        std::cerr << "Error: could not open output file: " << out << "\n";
        return 4;
    }
    ofs << "P6\n" << width << " " << height << "\n255\n";
    ofs.write(reinterpret_cast<const char*>(img.data()), std::streamsize(img.size()));
    ofs.close();

    return 0;
}
