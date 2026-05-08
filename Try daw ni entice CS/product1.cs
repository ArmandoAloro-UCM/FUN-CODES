using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Text;
using System.Windows.Forms;
using System.Data.SqlClient;

namespace APPSDEV_PROJECT
{
    public partial class product1 : Form
    {
        private string userEmail;

        public product1(string email = "")
        {
            InitializeComponent();
            userEmail = email;
        }

        private void button1_Click(object sender, EventArgs e)
        {
            // go back
            this.Close();

        }

        private void btn_confirm_Click(object sender, EventArgs e)
        {
            this.Hide();
            //Details
            string hotelName = "Lapu-Lapu Water Front Hotel";
            string location = "Lapu-Lapu";
            string roomType = "Standard Room";
            int tax = 250;

            string review = "AMAZING";
            int price = 2560;
            double score = 4.9;
            int reviewNum = 1305;
            int product = 1;

            int total = price + tax;


            Details d = new Details(hotelName, location, roomType, tax, review, price, score, reviewNum, product, total, userEmail); // pass email
            d.ShowDialog();
            this.Show();
        }
    }
}
