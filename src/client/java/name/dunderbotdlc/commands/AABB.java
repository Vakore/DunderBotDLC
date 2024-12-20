package name.dunderbotdlc.commands;

//From https://github.com/PrismarineJS/prismarine-physics/blob/master/lib/aabb.js
public class AABB {
    public double minX, minY, minZ;
    public double maxX, maxY, maxZ;

    public AABB(double x0, double y0, double z0, double x1, double y1, double z1) {
        this.minX = x0;
        this.minY = y0;
        this.minZ = z0;
        this.maxX = x1;
        this.maxY = y1;
        this.maxZ = z1;
    }

    public AABB clone() {
        return new AABB(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
    }

    public void floor() {
        this.minX = Math.floor(this.minX);
        this.minY = Math.floor(this.minY);
        this.minZ = Math.floor(this.minZ);
        this.maxX = Math.floor(this.maxX);
        this.maxY = Math.floor(this.maxY);
        this.maxZ = Math.floor(this.maxZ);
    }

    public AABB extend(double dx, double dy, double dz) {
        if (dx < 0) this.minX += dx;
        else this.maxX += dx;

        if (dy < 0) this.minY += dy;
        else this.maxY += dy;

        if (dz < 0) this.minZ += dz;
        else this.maxZ += dz;

        return this;
    }

    public AABB contract(double x, double y, double z) {
        this.minX += x;
        this.minY += y;
        this.minZ += z;
        this.maxX -= x;
        this.maxY -= y;
        this.maxZ -= z;
        return this;
    }

    public AABB expand(double x, double y, double z) {
        this.minX -= x;
        this.minY -= y;
        this.minZ -= z;
        this.maxX += x;
        this.maxY += y;
        this.maxZ += z;
        return this;
    }

    public AABB offset(double x, double y, double z) {
        this.minX += x;
        this.minY += y;
        this.minZ += z;
        this.maxX += x;
        this.maxY += y;
        this.maxZ += z;
        return this;
    }

    public double computeOffsetX(AABB other, double offsetX) {
        if (other.maxY > this.minY && other.minY < this.maxY && other.maxZ > this.minZ && other.minZ < this.maxZ) {
            if (offsetX > 0.0 && other.maxX <= this.minX) {
                offsetX = Math.min(this.minX - other.maxX, offsetX);
            } else if (offsetX < 0.0 && other.minX >= this.maxX) {
                offsetX = Math.max(this.maxX - other.minX, offsetX);
            }
        }
        return offsetX;
    }

    public double computeOffsetY(AABB other, double offsetY) {
        if (other.maxX > this.minX && other.minX < this.maxX && other.maxZ > this.minZ && other.minZ < this.maxZ) {
            if (offsetY > 0.0 && other.maxY <= this.minY) {
                offsetY = Math.min(this.minY - other.maxY, offsetY);
            } else if (offsetY < 0.0 && other.minY >= this.maxY) {
                offsetY = Math.max(this.maxY - other.minY, offsetY);
            }
        }
        return offsetY;
    }

    public double computeOffsetZ(AABB other, double offsetZ) {
        if (other.maxX > this.minX && other.minX < this.maxX && other.maxY > this.minY && other.minY < this.maxY) {
            if (offsetZ > 0.0 && other.maxZ <= this.minZ) {
                offsetZ = Math.min(this.minZ - other.maxZ, offsetZ);
            } else if (offsetZ < 0.0 && other.minZ >= this.maxZ) {
                offsetZ = Math.max(this.maxZ - other.minZ, offsetZ);
            }
        }
        return offsetZ;
    }

    public boolean intersects(AABB other) {
        return this.minX < other.maxX && this.maxX > other.minX &&
               this.minY < other.maxY && this.maxY > other.minY &&
               this.minZ < other.maxZ && this.maxZ > other.minZ;
    }
}

