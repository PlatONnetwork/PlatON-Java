package org.platon.core.rpc.wallet;

public class WalletAddressItem {

    public String address;

    public String name;

    public WalletAddressItem(String address, String name) {
        this.address = address;
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WalletAddressItem that = (WalletAddressItem) o;

        if (getAddress() != null ? !getAddress().equals(that.getAddress()) : that.getAddress() != null) return false;
        return getName() != null ? getName().equals(that.getName()) : that.getName() == null;
    }

    @Override
    public int hashCode() {
        int result = getAddress() != null ? getAddress().hashCode() : 0;
        result = 31 * result + (getName() != null ? getName().hashCode() : 0);
        return result;
    }
}
